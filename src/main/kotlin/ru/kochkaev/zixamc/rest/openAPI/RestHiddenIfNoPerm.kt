package ru.kochkaev.zixamc.rest.openAPI

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class RestHiddenIfNoPerm(val value: Boolean = true)