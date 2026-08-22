package com.greenwood.school.data.remote.api

import com.greenwood.school.data.remote.dto.ApiEnvelope
import com.greenwood.school.data.remote.dto.MyAccessDto
import retrofit2.http.GET

/**
 * What the signed-in user may actually do.
 *
 * <p>Open to every authenticated caller, and it has to be: this is how a session
 * learns what it is allowed to do, so gating it behind a permission would be
 * circular. It returns only facts about the caller themselves.
 */
interface AccessApi {

    @GET("me/access")
    suspend fun myAccess(): ApiEnvelope<MyAccessDto>
}
