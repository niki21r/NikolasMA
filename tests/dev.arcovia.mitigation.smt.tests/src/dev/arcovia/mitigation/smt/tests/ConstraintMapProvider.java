package dev.arcovia.mitigation.smt.tests;

import java.util.List;
import java.util.Map;

import org.dataflowanalysis.analysis.dsl.AnalysisConstraint;
import org.dataflowanalysis.analysis.dsl.constraint.ConstraintDSL;

public final class ConstraintMapProvider {

    private ConstraintMapProvider() {
    }

    private static final AnalysisConstraint ENTRY_VIA_GATEWAY_ONLY =
        new ConstraintDSL()
            .ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "gateway")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();

    private static final AnalysisConstraint NON_INTERNAL_GATEWAY =
        new ConstraintDSL()
            .ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "gateway")
            .withCharacteristic("Stereotype", "internal")
            .create();

    private static final AnalysisConstraint AUTHENTICATED_REQUEST =
        new ConstraintDSL()
            .ofData()
            .withoutLabel("Stereotype", "authenticated_request")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();

    private static final AnalysisConstraint TRANSFORMED_ENTRY =
        new ConstraintDSL()
            .ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "transform_identity_representation")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();

    private static final AnalysisConstraint TOKEN_VALIDATION =
        new ConstraintDSL()
            .ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "token_validation")
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .create();

    private static final AnalysisConstraint ENCRYPTED_ENTRY =
        new ConstraintDSL()
            .ofData()
            .withLabel("Stereotype", "entrypoint")
            .withoutLabel("Stereotype", "encrypted_connection")
            .neverFlows()
            .toVertex()
            .create();

    private static final AnalysisConstraint ENCRYPTED_INTERNALS =
        new ConstraintDSL()
            .ofData()
            .withLabel("Stereotype", "internal")
            .withoutLabel("Stereotype", "encrypted_connection")
            .neverFlows()
            .toVertex()
            .create();

    private static final AnalysisConstraint LOCAL_LOGGING =
        new ConstraintDSL()
            .ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "internal")
            .withoutCharacteristic("Stereotype", "local_logging")
            .create();

    private static final AnalysisConstraint LOG_SANITIZATION =
        new ConstraintDSL()
            .ofData()
            .neverFlows()
            .toVertex()
            .withCharacteristic("Stereotype", "local_logging")
            .withoutCharacteristic("Stereotype", "log_sanitization")
            .create();

    public static Map<Integer, List<AnalysisConstraint>> buildConstraintMap() {
        return Map.of(
            1,  List.of(ENTRY_VIA_GATEWAY_ONLY, NON_INTERNAL_GATEWAY),
            2,  List.of(AUTHENTICATED_REQUEST),
            4,  List.of(TRANSFORMED_ENTRY),
            5,  List.of(TOKEN_VALIDATION),
            7,  List.of(ENCRYPTED_ENTRY, ENTRY_VIA_GATEWAY_ONLY, NON_INTERNAL_GATEWAY),
            8,  List.of(ENCRYPTED_INTERNALS),
            10, List.of(LOCAL_LOGGING),
            11, List.of(LOCAL_LOGGING, LOG_SANITIZATION)
        );
    }
}
