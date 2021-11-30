package com.passbolt.mobile.android.feature.resources.update.fieldsgenerator

import com.passbolt.mobile.android.entity.resource.ResourceField

class ResourceFieldsComparator : Comparator<ResourceField> {

    override fun compare(resourceField: ResourceField?, other: ResourceField?) =
        order.indexOf(resourceField?.name).compareTo(order.indexOf(other?.name))

    private companion object {
        val order = linkedSetOf(
            FieldNamesMapper.NAME_FIELD,
            FieldNamesMapper.URI_FIELD,
            FieldNamesMapper.USERNAME_FIELD,
            FieldNamesMapper.PASSWORD_FIELD,
            FieldNamesMapper.SECRET_FIELD,
            FieldNamesMapper.DESCRIPTION_FIELD
        )
    }
}
