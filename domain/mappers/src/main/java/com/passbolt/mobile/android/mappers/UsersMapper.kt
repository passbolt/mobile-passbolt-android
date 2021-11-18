package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.UserDto
import com.passbolt.mobile.android.ui.GpgKey
import com.passbolt.mobile.android.ui.User

class UsersMapper {

    fun map(input: List<UserDto>) =
        input.map {
            User(
                it.id,
                GpgKey(it.gpgKey.armoredKey, it.gpgKey.fingerprint)
            )
        }
}
