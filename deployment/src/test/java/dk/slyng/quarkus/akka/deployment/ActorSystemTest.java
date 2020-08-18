package dk.slyng.quarkus.akka.deployment;

import akka.actor.typed.ActorSystem;
import dk.slyng.quarkus.akka.AkkaContainer;
import io.quarkus.test.QuarkusUnitTest;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ActorSystemTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(AkkaContainer.class));

    @Inject
    @Any
    Instance<ActorSystem> actorTypedSystem;
    @Inject
    @Any
    Instance<akka.actor.ActorSystem> actorClassicSystem;

    @Test
    public void testGettingTypedActorSystem() {
        assertNotNull(actorTypedSystem);
        List<ActorSystem> typedSystems = actorTypedSystem.stream().collect(Collectors.toList());
        assertEquals(1, typedSystems.size());

        ActorSystem actorSystem = typedSystems.get(0);
        assertEquals("default", actorSystem.name());
    }

    @Test
    public void testGettingClassicActorSystem() {
        assertNotNull(actorClassicSystem);
        List<akka.actor.ActorSystem> typedSystems = actorClassicSystem.stream().collect(Collectors.toList());
        assertEquals(1, typedSystems.size());

        akka.actor.ActorSystem actorSystem = typedSystems.get(0);
        assertEquals("default", actorSystem.name());
    }
}
