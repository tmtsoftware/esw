//@file:Repository("https://jitpack.io/")
//@file:DependsOn("com.github.tmtsoftware.esw:esw-ocs-dsl-kt_2.13:adc26faf3413a9e70a6627c397563e88ea04afb6")
//@file:DependsOn("com.github.tmtsoftware.esw:esw-ocs-app_2.13:adc26faf3413a9e70a6627c397563e88ea04afb6")

//package esw.ocs.scripts.examples.testData.scriptLoader

@file:Import("file1.seq.kts")

import esw.ocs.dsl.core.script

script {
println("XXX x1 = $x1")
//println("XXX x1 = $x1, x2 = $x2")

}