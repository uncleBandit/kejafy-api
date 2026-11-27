package com.pitahmzalendo.kejafy.listing.entity;

/** Who last set the scores on a listing's {@link EnvironmentProfile}. */
public enum EnvironmentSource {
    // The agent's own assessment when creating/editing the listing.
    AGENT_SELF_REPORTED,

    // An admin overrode/confirmed the scores (e.g. after a report that a
    // listing's self-reported flood risk was misleading). Takes
    // precedence in trust terms over AGENT_SELF_REPORTED, though nothing
    // currently blocks an agent from editing it back — see
    // ListingService.updateEnvironmentProfile.
    ADMIN_ASSESSED
}
