package dk.slyng.quarkus.akka;

import akka.Done;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Adapter;
import akka.discovery.Discovery;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.quarkus.runtime.configuration.ProfileManager;
import org.jboss.logging.Logger;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

@Recorder
public class AkkaRecorder {
    private static final Logger LOGGER = Logger.getLogger(AkkaRecorder.class.getName());

    private ActorSystem<Done> actorSystem;

    public void initializeAkkaSystem(AkkaConfig cfg,
                                     ShutdownContext shutdown) {
        final boolean devMode = ProfileManager.getLaunchMode() == LaunchMode.DEVELOPMENT;
        final boolean testMode = ProfileManager.getLaunchMode() == LaunchMode.TEST;

        LOGGER.debug("Initializing Akka ActorSystem");

        AkkaContainer akkaContainer = Arc.container().instance(AkkaContainer.class).get();
        if (akkaContainer == null) {
            throw new IllegalStateException("akka not initialized, AkkaContainer not found");
        }

        if (actorSystem != null) {
            LOGGER.fatal("ActorSystem already initialized!");
            return;
        }

        Config config;
        InputStream resourceAsStream = AkkaRecorder.class.getResourceAsStream("/application.conf");
        if (resourceAsStream != null) {
            config = ConfigFactory.load();
        } else {
            config = createConfig(cfg);
        }

        if (devMode || testMode) {
            config = createDevConfig(cfg).withFallback(config);
        }

        actorSystem = ActorSystem.create(akkaContainer.getGuardianBehavior().get().guardianBehavior(), cfg.systemName, config);

        shutdown.addShutdownTask(() -> {
            LOGGER.info("Shutting down akka system");
            if (actorSystem != null) {
                try {
                    actorSystem.terminate();
                    actorSystem.getWhenTerminated().toCompletableFuture().get(20, TimeUnit.SECONDS);
                } catch (InterruptedException | TimeoutException | ExecutionException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.error("Failed when trying to terminate Akka.", e);
                }
            }
        });
    }

    private Config createConfig(AkkaConfig cfg) {
        HashMap<String, Object> config = new HashMap<>();
        config.put("akka.loglevel", "INFO");
        config.put("akka.loggers", Collections.singletonList("akka.event.slf4j.Slf4jLogger"));
        config.put("akka.logging-filter", "akka.event.slf4j.Slf4jLoggingFilter");

        if (cfg.enableCluster) {
            config.put("akka.actor.provider", "cluster");
            config.put("akka.remote.artery.canonical.hostname", cfg.remoteHostname);
            config.put("akka.remote.artery.canonical.port", cfg.remotePort);

            cfg.discovery.ifPresent(akkaDiscovery -> {
                switch (akkaDiscovery) {
                    case KUBERNETES_API:
                        Discovery.get(actorSystem).loadServiceDiscovery("kubernetes-api");
                        cfg.kubernetesApi.ifPresent(kubernetesApiConfig -> {
                            kubernetesApiConfig.podNamespace.ifPresent(pn ->
                                    config.put("akka.discovery.kubernetes-api.pod-namespace", pn));
                            kubernetesApiConfig.podLabelSelector.ifPresent(pls ->
                                    config.put("akka.discovery.kubernetes-api.pod-label-selector", pls));

                        });
                        break;
                }
                ClusterBootstrap.get(Adapter.toClassic(actorSystem)).start();
            });
        }

        return ConfigFactory.parseMap(config);
    }

    private Config createDevConfig(AkkaConfig cfg) {
        HashMap<String, Object> config = new HashMap<>();

        if (cfg.enableCluster) {
            LOGGER.info("Configure Akka Cluster to run as single node when in dev mode.");
            config.put("akka.remote.artery.canonical.hostname", "localhost");
            config.put("akka.cluster.seed-nodes", Collections.singletonList("akka://" + cfg.systemName + "@localhost:" + cfg.remotePort));
        }

        return ConfigFactory.parseMap(config);
    }

    public Supplier<ActorSystem<Done>> actorSystemSupplier() {
        return () -> actorSystem;
    }

    public Supplier<akka.actor.ActorSystem> actorSystemClassicSupplier() {
        return () -> Adapter.toClassic(actorSystem);
    }

    public Supplier<AkkaGuardianBehavior> akkaGuardianBehaviorSupplier() {
        return AkkaGuardianBehavior::new;
    }
}
