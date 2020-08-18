package dk.slyng.quarkus.akka;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.DefaultConverter;

import java.util.Optional;

@ConfigRoot(name = "akka", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class AkkaConfig {

    /**
     * Actor System name
     */
    @ConfigItem(defaultValue = "default")
    public String systemName;

    /**
     * To enable Akka Cluster, default is disabled.
     */
    @ConfigItem(defaultValue = "false")
    public boolean enableCluster;

    /**
     * Which hostname the ActorSystem will listen for remote connections, default is 0.0.0.0.
     */
    @ConfigItem(defaultValue = "0.0.0.0")
    public String remoteHostname;

    /**
     * Which port the ActorSystem will listen on for remote connections, default is 2551.
     */
    @ConfigItem(defaultValue = "2551")
    Integer remotePort;

    /**
     * Which akka cluster discovery method to use.
     */
    @DefaultConverter
    @ConfigItem
    public Optional<AkkaDiscovery> discovery;

    public enum AkkaDiscovery {
        KUBERNETES_API
    }

    /**
     * Configuration of the Akka KubernetesApi Discovery.
     */
    @ConfigItem
    public Optional<KubernetesApiConfig> kubernetesApi;

    @ConfigGroup
    public class KubernetesApiConfig {
        public Optional<String> podNamespace;
        public Optional<String> podLabelSelector;
    }
}
