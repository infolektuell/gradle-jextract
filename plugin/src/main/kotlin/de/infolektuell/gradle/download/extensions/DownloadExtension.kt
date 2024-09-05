package de.infolektuell.gradle.download.extensions

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class DownloadExtension @Inject constructor(objects: ObjectFactory) {
    abstract val targetDirectory: DirectoryProperty
    val resources: NamedDomainObjectContainer<ResourceHandler> = objects.domainObjectContainer(ResourceHandler::class.java)
    fun resources(action: Action<in NamedDomainObjectContainer<ResourceHandler>>) {
        action.execute(resources)
    }
    companion object {
        const val EXTENSION_NAME = "download"
    }
}
