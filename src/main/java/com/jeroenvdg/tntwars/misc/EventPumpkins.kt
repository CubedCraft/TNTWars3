package com.jeroenvdg.tntwars.misc

import com.jeroenvdg.tntwars.game.Team
import com.sk89q.worldedit.math.BlockVector3

/** Only pumpkins placed by the event can score, and each position can score once. */
class EventPumpkins {
    private val pumpkins = HashMap<BlockVector3, Team>()

    fun add(position: BlockVector3, team: Team) {
        pumpkins[position] = team
    }

    fun remove(position: BlockVector3) {
        pumpkins.remove(position)
    }

    fun destroy(position: BlockVector3, attackingTeam: Team?): Boolean {
        val defendingTeam = pumpkins.remove(position) ?: return false
        return attackingTeam?.isGameTeam == true && defendingTeam.isGameTeam && attackingTeam != defendingTeam
    }

    fun clear() = pumpkins.clear()
}
