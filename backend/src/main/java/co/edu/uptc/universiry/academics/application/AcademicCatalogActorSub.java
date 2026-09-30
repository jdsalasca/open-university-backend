package co.edu.uptc.universiry.academics.application;

import co.edu.uptc.universiry.academics.domain.AcademicCatalogLimits;

public final class AcademicCatalogActorSub {

    private AcademicCatalogActorSub() {
    }

    public static String require(String actorSub) {
        if (actorSub == null || actorSub.isBlank()
                || actorSub.codePointCount(0, actorSub.length()) > AcademicCatalogLimits.MAX_ACTOR_SUB_LENGTH) {
            throw new AcademicCatalogActorException();
        }
        return actorSub;
    }
}
