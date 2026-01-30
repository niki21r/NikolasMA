mvn -pl tests/dev.arcovia.mitigation.smt.tests \
    org.eclipse.tycho:tycho-surefire-plugin:test \
    -Dtest=* \
    -Dtycho.surefire.captureConsole=false
