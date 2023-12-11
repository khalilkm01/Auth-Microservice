package models.infrastructure

sealed trait EventTopics
case object `org.thirty7.auth.login`          extends EventTopics
case object `org.thirty7.auth.contact_number` extends EventTopics
case object `org.thirty7.auth.email`          extends EventTopics
