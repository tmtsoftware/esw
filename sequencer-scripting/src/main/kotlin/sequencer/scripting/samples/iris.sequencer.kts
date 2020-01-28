script {

    val prefix = "OCS.IRIS_darkMode"
    val hcd1 = Hcd(IRIS, "filter.wheel1", 10.minutes)
    val hcd2 = Hcd(IRIS, "filter.wheel2", 10.minutes)
    val hcd3 = Hcd(IRIS, "filter.wheel3", 10.minutes)

    onSetup("setup-iris-hcds") {
        // send 3 setup commands to 3 HCD's in parallel
        par(
                { hcd1.submitAndWait(Setup(prefix, "move-10")) },
                { hcd2.submitAndWait(Setup(prefix, "move-10")) },
                { hcd3.submitAndWait(Setup(prefix, "move-10")) }
        )
    }
}