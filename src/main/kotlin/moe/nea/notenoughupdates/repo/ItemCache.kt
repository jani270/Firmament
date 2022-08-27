package moe.nea.notenoughupdates.repo

import com.mojang.serialization.Dynamic
import io.github.cottonmc.cotton.gui.client.CottonHud
import io.github.moulberry.repo.IReloadable
import io.github.moulberry.repo.NEURepository
import io.github.moulberry.repo.data.NEUItem
import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.io.path.absolutePathString
import kotlin.io.path.writer
import net.minecraft.client.resource.language.I18n
import net.minecraft.datafixer.Schemas
import net.minecraft.datafixer.TypeReferences
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtOps
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import moe.nea.notenoughupdates.NotEnoughUpdates
import moe.nea.notenoughupdates.util.LegacyTagParser
import moe.nea.notenoughupdates.util.appendLore

object ItemCache : IReloadable {
    val dfuLog = Path.of("logs/dfulog.txt")
    private val cache: MutableMap<String, ItemStack> = ConcurrentHashMap()
    private val df = Schemas.getFixer()
    private val dfuHandle = PrintWriter(dfuLog.writer())
    var isFlawless = true
        private set

    private fun NEUItem.get10809CompoundTag(): NbtCompound = NbtCompound().apply {
        put("tag", LegacyTagParser.parse(nbttag))
        putString("id", minecraftItemId)
        putByte("Count", 1)
        putShort("Damage", damage.toShort())
    }

    private fun NbtCompound.transformFrom10809ToModern(): NbtCompound? =
        try {
            df.update(
                TypeReferences.ITEM_STACK,
                Dynamic(NbtOps.INSTANCE, this),
                -1,
                2975
            ).value as NbtCompound
        } catch (e: Exception) {
            if (isFlawless)
                NotEnoughUpdates.logger.error("Failed to run data fixer an item. Check ${dfuLog.absolutePathString()} for more information")
            isFlawless = false
            e.printStackTrace(dfuHandle)
            null
        }

    private fun NEUItem.asItemStackNow(): ItemStack {
        val oldItemTag = get10809CompoundTag()
        val modernItemTag = oldItemTag.transformFrom10809ToModern()
            ?: return ItemStack(Items.PAINTING).apply {
                setCustomName(Text.literal(this@asItemStackNow.displayName))
                appendLore(listOf(Text.translatable("notenoughupdates.repo.brokenitem", skyblockItemId)))
            }
        val itemInstance = ItemStack.fromNbt(modernItemTag)
        if (itemInstance.nbt?.contains("Enchantments") == true) {
            itemInstance.enchantments.add(NbtCompound())
        }
        return itemInstance
    }

    fun NEUItem.asItemStack(): ItemStack {
        var s = cache[this.skyblockItemId]
        if (s == null) {
            s = asItemStackNow()
            cache[this.skyblockItemId] = s
        }
        return s
    }

    fun NEUItem.getIdentifier() =
        Identifier("skyblockitem", skyblockItemId.lowercase().replace(";", "__"))


    var job: Job? = null

    override fun reload(repository: NEURepository) {
        val j = job
        if (j != null && j.isActive) {
            j.cancel()
        }
        cache.clear()
        isFlawless = true

        job = NotEnoughUpdates.coroutineScope.launch {
            val items = repository.items?.items
            if (items == null) {
                CottonHud.remove(RepoManager.progressBar)
                return@launch
            }
            val recacheItems = I18n.translate("notenoughupdates.repo.cache")
            RepoManager.progressBar.reportProgress(recacheItems, 0, items.size)
            CottonHud.add(RepoManager.progressBar)
            var i = 0
            items.values.forEach {
                it.asItemStack() // Rebuild cache
                RepoManager.progressBar.reportProgress(recacheItems, i++, items.size)
            }
            CottonHud.remove(RepoManager.progressBar)
        }
    }
}
