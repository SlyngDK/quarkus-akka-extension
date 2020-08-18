package dk.slyng.quarkus.akka;

import akka.Done;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.Behaviors;

import javax.enterprise.context.Dependent;

@Dependent
public class AkkaGuardianBehavior {
    public Behavior<Done> guardianBehavior() {
        return Behaviors.empty();
    }
}
