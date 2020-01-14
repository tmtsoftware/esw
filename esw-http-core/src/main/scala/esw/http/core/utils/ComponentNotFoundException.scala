package esw.http.core.utils

import csw.location.models.ComponentId

case class ComponentNotFoundException(componentId: ComponentId)
    extends RuntimeException(s"No component is registered with id $componentId")
