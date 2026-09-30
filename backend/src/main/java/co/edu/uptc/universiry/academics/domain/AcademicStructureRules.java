package co.edu.uptc.universiry.academics.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AcademicStructureRules {

    private AcademicStructureRules() {
    }

    public static void validateInterval(LocalDate from, LocalDate through, String field) {
        if (from == null || through != null && through.isBefore(from)) {
            throw new IllegalArgumentException(field + ".validity is invalid");
        }
    }

    public static boolean overlaps(LocalDate firstFrom, LocalDate firstThrough,
                                   LocalDate secondFrom, LocalDate secondThrough) {
        return (firstThrough == null || !firstThrough.isBefore(secondFrom))
                && (secondThrough == null || !secondThrough.isBefore(firstFrom));
    }

    public static boolean containedBy(LocalDate childFrom, LocalDate childThrough,
                                      LocalDate parentFrom, LocalDate parentThrough) {
        return !childFrom.isBefore(parentFrom)
                && (parentThrough == null || childThrough != null && !childThrough.isAfter(parentThrough));
    }

    public static boolean createsCycle(AcademicOrganizationRelation candidate,
                                       List<AcademicOrganizationRelation> existing) {
        return hasPath(candidate.childUnitId(), candidate.parentUnitId(),
                candidate.validFrom(), candidate.validThrough(), existing, new HashSet<>());
    }

    public static boolean createsCycle(AcademicSiteRelation candidate, List<AcademicSiteRelation> existing) {
        return hasSitePath(candidate.childSiteId(), candidate.parentSiteId(),
                candidate.validFrom(), candidate.validThrough(), existing, new HashSet<>());
    }

    private static boolean hasPath(UUID current, UUID target, LocalDate from, LocalDate through,
                                   List<AcademicOrganizationRelation> edges, Set<UUID> path) {
        if (current.equals(target)) return true;
        if (!path.add(current)) return false;
        try {
            for (AcademicOrganizationRelation edge : edges) {
                if (!edge.parentUnitId().equals(current)
                        || !overlaps(from, through, edge.validFrom(), edge.validThrough())) continue;
                LocalDate intersectionFrom = from.isAfter(edge.validFrom()) ? from : edge.validFrom();
                LocalDate intersectionThrough = minEnd(through, edge.validThrough());
                if (hasPath(edge.childUnitId(), target, intersectionFrom, intersectionThrough, edges, path)) {
                    return true;
                }
            }
            return false;
        } finally {
            path.remove(current);
        }
    }

    private static boolean hasSitePath(UUID current, UUID target, LocalDate from, LocalDate through,
                                       List<AcademicSiteRelation> edges, Set<UUID> path) {
        if (current.equals(target)) return true;
        if (!path.add(current)) return false;
        try {
            for (AcademicSiteRelation edge : edges) {
                if (!edge.parentSiteId().equals(current)
                        || !overlaps(from, through, edge.validFrom(), edge.validThrough())) continue;
                LocalDate intersectionFrom = from.isAfter(edge.validFrom()) ? from : edge.validFrom();
                LocalDate intersectionThrough = minEnd(through, edge.validThrough());
                if (hasSitePath(edge.childSiteId(), target, intersectionFrom, intersectionThrough, edges, path)) {
                    return true;
                }
            }
            return false;
        } finally {
            path.remove(current);
        }
    }

    private static LocalDate minEnd(LocalDate first, LocalDate second) {
        if (first == null) return second;
        if (second == null) return first;
        return first.isBefore(second) ? first : second;
    }
}
