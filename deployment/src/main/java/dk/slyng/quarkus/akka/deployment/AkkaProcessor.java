package dk.slyng.quarkus.akka.deployment;

import akka.actor.typed.ActorSystem;
import dk.slyng.quarkus.akka.AkkaConfig;
import dk.slyng.quarkus.akka.AkkaContainer;
import dk.slyng.quarkus.akka.AkkaGuardianBehavior;
import dk.slyng.quarkus.akka.AkkaRecorder;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.SyntheticBeanBuildItem;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import org.jboss.jandex.DotName;

import javax.enterprise.inject.Default;
import javax.inject.Singleton;

class AkkaProcessor {

    private static final String FEATURE = "akka";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(value = ExecutionTime.RUNTIME_INIT)
    ServiceStartBuildItem build(AkkaRecorder recorder, AkkaConfig config,
                                BuildProducer<SyntheticBeanBuildItem> syntheticBeanBuildItemBuildProducer,
                                ShutdownContextBuildItem shutdown) {
        recorder.initializeAkkaSystem(config, shutdown);

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configurator = SyntheticBeanBuildItem.configure(ActorSystem.class)
                .addType(DotName.createSimple(ActorSystem.class.getName()))
                .scope(Singleton.class)
                .addQualifier(Default.class)
                .setRuntimeInit()
                .unremovable()
                .supplier(recorder.actorSystemSupplier());
        syntheticBeanBuildItemBuildProducer.produce(configurator.done());

        SyntheticBeanBuildItem.ExtendedBeanConfigurator configuratorClassic = SyntheticBeanBuildItem.configure(akka.actor.ActorSystem.class)
                .scope(Singleton.class)
                .addQualifier(Default.class)
                .setRuntimeInit()
                .unremovable()
                .supplier(recorder.actorSystemClassicSupplier());

        syntheticBeanBuildItemBuildProducer.produce(configuratorClassic.done());

        return new ServiceStartBuildItem(FEATURE);
    }

    @BuildStep
    void buildContainerBean(BuildProducer<AdditionalBeanBuildItem> beans, BuildProducer<FeatureBuildItem> features) {
        beans.produce(AdditionalBeanBuildItem.unremovableOf(AkkaContainer.class));
    }

    @BuildStep
    void addBeans(BuildProducer<AdditionalBeanBuildItem> additionalBeans) {
        additionalBeans.produce(AdditionalBeanBuildItem.builder().addBeanClasses(AkkaGuardianBehavior.class).setUnremovable()
                .setDefaultScope(DotNames.SINGLETON).build());
    }
}
