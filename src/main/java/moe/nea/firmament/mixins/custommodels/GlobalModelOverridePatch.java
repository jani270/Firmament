
package moe.nea.firmament.mixins.custommodels;

import moe.nea.firmament.features.texturepack.CustomGlobalTextures;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemRenderer.class)
public abstract class GlobalModelOverridePatch {

	@Shadow
	@Final
	private ItemModels models;

	@Inject(method = "getModel(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/LivingEntity;I)Lnet/minecraft/client/render/model/BakedModel;", at = @At("HEAD"), cancellable = true)
	private void overrideGlobalModel(
		ItemStack stack, World world, LivingEntity entity,
		int seed, CallbackInfoReturnable<BakedModel> cir) {
		CustomGlobalTextures.replaceGlobalModel(this.models, stack, cir);
	}
}
