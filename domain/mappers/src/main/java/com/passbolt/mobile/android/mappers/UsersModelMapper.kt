package com.passbolt.mobile.android.mappers

import com.passbolt.mobile.android.dto.response.UserDto
import com.passbolt.mobile.android.entity.user.User
import com.passbolt.mobile.android.entity.user.UserGpgKey
import com.passbolt.mobile.android.entity.user.UserProfile
import com.passbolt.mobile.android.ui.GpgKeyModel
import com.passbolt.mobile.android.ui.UserModel
import com.passbolt.mobile.android.ui.UserProfileModel
import com.passbolt.mobile.android.ui.UserWithAvatar
import java.time.ZonedDateTime

class UsersModelMapper {

    fun map(input: List<UserDto>) =
        input.map {
            UserModel(
                it.id,
                it.username,
                GpgKeyModel(
                    it.gpgKey.armoredKey,
                    it.gpgKey.fingerprint,
                    it.gpgKey.bits,
                    it.gpgKey.uid,
                    it.gpgKey.keyId,
                    it.gpgKey.type,
                    it.gpgKey.expires?.let { expires -> ZonedDateTime.parse(expires) }
                ),
                UserProfileModel(it.profile?.firstName, it.profile?.lastName, it.profile?.avatar?.url?.medium)
            )
        }

    fun map(input: UserModel) =
        User(
            input.id,
            input.userName,
            UserProfile(
                input.profile.firstName,
                input.profile.lastName,
                input.profile.avatarUrl
            ),
            UserGpgKey(
                input.gpgKey.armoredKey,
                input.gpgKey.bits,
                input.gpgKey.uid,
                input.gpgKey.keyId,
                input.gpgKey.fingerprint,
                input.gpgKey.type,
                input.gpgKey.expires
            )
        )

    fun map(input: User) =
        UserModel(
            input.id,
            input.userName,
            GpgKeyModel(
                input.gpgKey.armoredKey,
                input.gpgKey.fingerprint,
                input.gpgKey.bits,
                input.gpgKey.uid,
                input.gpgKey.keyId,
                input.gpgKey.type,
                input.gpgKey.expires
            ),
            UserProfileModel(
                input.profile.firstName,
                input.profile.lastName,
                input.profile.avatarUrl
            )
        )

    fun mapToUserWithAvatar(input: UserModel) =
        UserWithAvatar(
            input.id,
            input.profile.firstName.orEmpty(),
            input.profile.lastName.orEmpty(),
            input.userName,
            input.profile.avatarUrl
        )
}
