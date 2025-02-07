

package moe.nea.firmament.mixins;

import moe.nea.firmament.events.CustomItemModelEvent;
import net.minecraft.client.render.item.ItemModels;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedModelManager;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(ItemModels.class)
public class CustomModelEventPatch {

	@Inject(method = "getModel(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/client/render/model/BakedModel;", at = @At("HEAD"), cancellable = true)
	public void onGetModel(ItemStack stack, CallbackInfoReturnable<BakedModel> cir) {
		var model = CustomItemModelEvent.getModel(stack, (ItemModels) (Object) this);
		if (model != null)
			cir.setReturnValue(model);
	}
}
