package com.dnschanger.app

import android.content.Context
import android.content.pm.PackageManager

data class KGame(val name: String, val pkgs: List<String>, val hosts: List<String>)

object Games {
    val generic = KGame("اینترنت عمومی", emptyList(), listOf("google.com", "cloudflare.com", "youtube.com"))

    val known = listOf(
        KGame("Mobile Legends", listOf("com.mobile.legends"), listOf("mobilelegends.com", "m.mobilelegends.com", "moonton.com")),
        KGame("PUBG Mobile", listOf("com.tencent.ig", "com.pubg.krmobile", "com.tencent.iglite", "com.pubg.imobile"), listOf("pubgmobile.com", "igamecj.com", "proximabeta.com")),
        KGame("Call of Duty Mobile", listOf("com.activision.callofduty.shooter"), listOf("callofduty.com", "activision.com")),
        KGame("Call of Duty Warzone Mobile", listOf("com.activision.callofduty.warzone"), listOf("callofduty.com", "activision.com")),
        KGame("Free Fire", listOf("com.dts.freefireth", "com.dts.freefiremax"), listOf("ff.garena.com", "garena.com", "freefiremobile.com")),
        KGame("Clash of Clans", listOf("com.supercell.clashofclans"), listOf("supercell.com", "clashofclans.com")),
        KGame("Clash Royale", listOf("com.supercell.clashroyale"), listOf("supercell.com", "clashroyale.com")),
        KGame("Brawl Stars", listOf("com.supercell.brawlstars"), listOf("brawlstars.com", "supercell.com")),
        KGame("Genshin Impact", listOf("com.miHoYo.GenshinImpact"), listOf("hoyoverse.com", "genshin.hoyoverse.com", "mihoyo.com")),
        KGame("Honkai: Star Rail", listOf("com.HoYoverse.hkrpgoversea"), listOf("hoyoverse.com", "hsr.hoyoverse.com")),
        KGame("Roblox", listOf("com.roblox.client"), listOf("roblox.com")),
        KGame("Minecraft", listOf("com.mojang.minecraftpe"), listOf("minecraft.net", "mojang.com")),
        KGame("Wild Rift", listOf("com.riotgames.league.wildrift"), listOf("leagueoflegends.com", "riotgames.com")),
        KGame("Honor of Kings", listOf("com.levelinfinite.sgameGlobal"), listOf("levelinfinite.com", "tencent.com")),
        KGame("Among Us", listOf("com.innersloth.spacemafia"), listOf("innersloth.com", "among.us")),
        KGame("eFootball", listOf("jp.konami.pesam"), listOf("konami.net", "konami.com")),
        KGame("EA SPORTS FC Mobile", listOf("com.ea.gp.fifamobile"), listOf("ea.com", "easports.com")),
        KGame("Standoff 2", listOf("com.axlebolt.standoff2"), listOf("axlebolt.com", "standoff2.com")),
        KGame("8 Ball Pool", listOf("com.miniclip.eightballpool"), listOf("miniclip.com", "8ballpool.com")),
        KGame("Pokémon GO", listOf("com.nianticlabs.pokemongo"), listOf("nianticlabs.com", "pokemongo.com")),
        KGame("Zooba", listOf("com.wildlife.games.battle.royale.free.zooba"), listOf("wildlifestudios.com")),
        KGame("Hearthstone", listOf("com.blizzard.wtcg.hearthstone"), listOf("blizzard.com", "battle.net"))
    )

    /** بازی‌های شناخته‌شده‌ای که روی گوشی نصب هستند */
    fun installed(ctx: Context): List<KGame> {
        val pm = ctx.packageManager
        return known.filter { g ->
            g.pkgs.any {
                try { pm.getPackageInfo(it, 0); true } catch (_: PackageManager.NameNotFoundException) { false } catch (_: Exception) { false }
            }
        }
    }
}

/** حدس دامنه‌های هر برنامه برای تست DNS */
object AppHosts {
    private val map = listOf(
        "instagram" to listOf("instagram.com", "i.instagram.com", "cdninstagram.com"),
        "telegram" to listOf("telegram.org", "web.telegram.org", "t.me"),
        "whatsapp" to listOf("whatsapp.com", "web.whatsapp.com", "whatsapp.net"),
        "google.android.youtube" to listOf("youtube.com", "googlevideo.com", "ytimg.com"),
        "musically" to listOf("tiktok.com", "tiktokv.com", "tiktokcdn.com"),
        "ugc.trill" to listOf("tiktok.com", "tiktokv.com", "tiktokcdn.com"),
        "facebook" to listOf("facebook.com", "fbcdn.net", "graph.facebook.com"),
        "twitter" to listOf("twitter.com", "twimg.com", "x.com"),
        "spotify" to listOf("spotify.com", "scdn.co", "api.spotify.com"),
        "netflix" to listOf("netflix.com", "nflxvideo.net", "nflximg.net"),
        "discord" to listOf("discord.com", "discordapp.com", "discord.gg"),
        "snapchat" to listOf("snapchat.com", "sc-cdn.net", "feelinsonice.com"),
        "zoom" to listOf("zoom.us", "zoom.com"),
        "twitch" to listOf("twitch.tv", "ttvnw.net", "twitchcdn.net"),
        "reddit" to listOf("reddit.com", "redd.it", "redditstatic.com"),
        "android.vending" to listOf("play.google.com", "gstatic.com", "googleapis.com"),
        "android.chrome" to listOf("google.com", "gstatic.com", "googleapis.com"),
        "com.google." to listOf("google.com", "gstatic.com", "googleapis.com"),
        "digikala" to listOf("digikala.com", "api.digikala.com", "dkstatics-public.digikala.com"),
        "snapp" to listOf("snapp.ir", "app.snapp.taxi", "snappfood.ir"),
        "divar" to listOf("divar.ir", "api.divar.ir"),
        "aparat" to listOf("aparat.com", "static.aparat.com"),
        "rubika" to listOf("rubika.ir", "messengerg2c1.iranlms.ir"),
        "nasim" to listOf("bale.ai", "web.bale.ai"),
        "eitaa" to listOf("eitaa.com", "web.eitaa.com"),
        "balad" to listOf("balad.ir", "api.balad.ir"),
        "shad" to listOf("shad.ir", "rubika.ir")
    )

    fun forPackage(pkg: String, label: String): List<String> {
        Games.known.firstOrNull { g -> g.pkgs.contains(pkg) }?.let { return it.hosts }
        val p = pkg.lowercase()
        for ((k, v) in map) if (p.contains(k)) return v
        val parts = p.split(".")
        val generic = setOf("android", "app", "apps", "mobile", "client", "lite", "messenger", "main", "release", "free", "pro", "mobi", "droid", "official", "global", "gp", "play")
        val first = parts.first()
        val rest = parts.drop(1).filter { it !in generic && it.length > 2 }
        val name = rest.firstOrNull() ?: parts.getOrNull(1) ?: first
        val dom = when (first) {
            "ir" -> "$name.ir"
            "org" -> "$name.org"
            "net" -> "$name.net"
            "tv" -> "$name.tv"
            "io" -> "$name.io"
            else -> "$name.com"
        }
        return listOf(dom, "www.$dom", "api.$dom").distinct()
    }
}
