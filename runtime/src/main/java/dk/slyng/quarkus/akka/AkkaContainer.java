package dk.slyng.quarkus.akka;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Instance;
import javax.inject.Inject;

@ApplicationScoped
public class AkkaContainer {

    @Inject
    Instance<AkkaGuardianBehavior> guardianBehavior;

    public Instance<AkkaGuardianBehavior> getGuardianBehavior() {
        return guardianBehavior;
    }
}
