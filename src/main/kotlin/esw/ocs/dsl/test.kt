package esw.ocs.dsl

import kotlin.reflect.KClass

abstract class Base(val seed: String) {

    abstract fun foo(): Int
    abstract fun bar(): String

    fun create(foo: Int, bar: String) =
        object : Base(seed) {
            override fun foo(): Int = foo

            override fun bar(): String = bar
        }

    fun printMe() = println("Foo = ${foo()}, Bar = ${bar()}}")
}

abstract class Base1(seed: String) : Base(seed) {
    init {
        println("Base1")
    }
}

abstract class Base2(seed: String) : Base(seed) {
    init {
        println("Base2")
    }
}

fun main() {
    fun <T : Base> loadScripts(vararg scripts: KClass<out T>) {
        val s = scripts.map {
            it.java.getConstructor(String::class.java).newInstance("Seed")
                .create(10, "bar")
        }
        s.forEach {
            it.printMe()
        }
    }

    loadScripts(
        Base1::class,
        Base2::class
    )
}