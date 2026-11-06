package com.roosterplanner.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.roosterplanner.app.R

/** Brand logo for a location, based on its name (e.g. "Esso Isselt", "Shell Serva"). */
@DrawableRes
fun locationLogoRes(locationName: String?): Int? {
    val name = locationName?.lowercase() ?: return null
    return when {
        "esso" in name -> R.drawable.logo_esso
        "shell" in name -> R.drawable.logo_shell
        else -> null
    }
}

/** Shows the location's brand logo, or nothing when the location has no known brand. */
@Composable
fun LocationLogo(locationName: String?, size: Dp = 20.dp, modifier: Modifier = Modifier) {
    val res = locationLogoRes(locationName) ?: return
    Image(
        painter = painterResource(res),
        contentDescription = locationName,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size)
    )
}
