/*
 * SPDX-FileCopyrightText: 2023 Linnea Gräf <nea@nea.moe>
 * SPDX-FileCopyrightText: 2024 Linnea Gräf <nea@nea.moe>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package moe.nea.firmament.features

import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import moe.nea.firmament.Firmament
import moe.nea.firmament.annotations.generated.AllSubscriptions
import moe.nea.firmament.events.FeaturesInitializedEvent
import moe.nea.firmament.events.FirmamentEvent
import moe.nea.firmament.events.subscription.Subscription
import moe.nea.firmament.features.chat.AutoCompletions
import moe.nea.firmament.features.chat.ChatLinks
import moe.nea.firmament.features.chat.QuickCommands
import moe.nea.firmament.features.debug.DebugView
import moe.nea.firmament.features.debug.DeveloperFeatures
import moe.nea.firmament.features.debug.MinorTrolling
import moe.nea.firmament.features.debug.PowerUserTools
import moe.nea.firmament.features.diana.DianaWaypoints
import moe.nea.firmament.features.fixes.CompatibliltyFeatures
import moe.nea.firmament.features.fixes.Fixes
import moe.nea.firmament.features.inventory.CraftingOverlay
import moe.nea.firmament.features.inventory.ItemRarityCosmetics
import moe.nea.firmament.features.inventory.PriceData
import moe.nea.firmament.features.inventory.SaveCursorPosition
import moe.nea.firmament.features.inventory.SlotLocking
import moe.nea.firmament.features.inventory.buttons.InventoryButtons
import moe.nea.firmament.features.inventory.storageoverlay.StorageOverlay
import moe.nea.firmament.features.mining.PickaxeAbility
import moe.nea.firmament.features.mining.PristineProfitTracker
import moe.nea.firmament.features.texturepack.CustomSkyBlockTextures
import moe.nea.firmament.features.world.FairySouls
import moe.nea.firmament.features.world.Waypoints
import moe.nea.firmament.util.data.DataHolder

object FeatureManager : DataHolder<FeatureManager.Config>(serializer(), "features", ::Config) {
    @Serializable
    data class Config(
        val enabledFeatures: MutableMap<String, Boolean> = mutableMapOf()
    )

    private val features = mutableMapOf<String, FirmamentFeature>()

    val allFeatures: Collection<FirmamentFeature> get() = features.values

    private var hasAutoloaded = false

    init {
        autoload()
    }

    fun autoload() {
        synchronized(this) {
            if (hasAutoloaded) return
            loadFeature(MinorTrolling)
            loadFeature(FairySouls)
            loadFeature(AutoCompletions)
            // TODO: loadFeature(FishingWarning)
            loadFeature(SlotLocking)
            loadFeature(StorageOverlay)
            loadFeature(PristineProfitTracker)
            loadFeature(CraftingOverlay)
            loadFeature(PowerUserTools)
            loadFeature(Waypoints)
            loadFeature(ChatLinks)
            loadFeature(InventoryButtons)
            loadFeature(CompatibliltyFeatures)
            loadFeature(QuickCommands)
            loadFeature(SaveCursorPosition)
            loadFeature(CustomSkyBlockTextures)
            loadFeature(PriceData)
            loadFeature(Fixes)
            loadFeature(DianaWaypoints)
            loadFeature(ItemRarityCosmetics)
            loadFeature(PickaxeAbility)
            if (Firmament.DEBUG) {
                loadFeature(DeveloperFeatures)
                loadFeature(DebugView)
            }
            allFeatures.forEach { it.config }
            subscribeEvents()
            FeaturesInitializedEvent.publish(FeaturesInitializedEvent(allFeatures.toList()))
            hasAutoloaded = true
        }
    }

    private fun subscribeEvents() {
        AllSubscriptions.provideSubscriptions {
            subscribeSingleEvent(it)
        }
    }

    private fun <T : FirmamentEvent> subscribeSingleEvent(it: Subscription<T>) {
        if (it.owner in features.values) { // TODO: better check here, somehow. probably implement some interface method
            it.eventBus.subscribe(false, it.invoke) // TODO: pass through receivesCancelled from the annotation
        } else {
            Firmament.logger.error("Ignoring event listener for ${it.eventBus} in ${it.owner}")
        }
    }

    fun loadFeature(feature: FirmamentFeature) {
        synchronized(features) {
            if (feature.identifier in features) {
                Firmament.logger.error("Double registering feature ${feature.identifier}. Ignoring second instance $feature")
                return
            }
            features[feature.identifier] = feature
            feature.onLoad()
        }
    }

    fun isEnabled(identifier: String): Boolean? =
        data.enabledFeatures[identifier]


    fun setEnabled(identifier: String, value: Boolean) {
        data.enabledFeatures[identifier] = value
        markDirty()
    }

}
