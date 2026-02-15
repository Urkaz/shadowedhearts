package com.jayemceekay.shadowedhearts.content.items;

import com.cobblemon.mod.common.api.callback.MoveSelectCallbacks;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveSet;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.jayemceekay.shadowedhearts.Shadowedhearts;
import com.jayemceekay.shadowedhearts.common.shadow.ShadowAspectUtil;
import com.jayemceekay.shadowedhearts.config.HeartGaugeConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class ShadowScale extends Item {

    public ShadowScale(Properties properties) {
        super(properties);
    }


    @Override
    public InteractionResult interactLivingEntity(ItemStack itemStack, Player player, LivingEntity livingEntity, InteractionHand interactionHand) {
        Level level = player.level();
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(livingEntity instanceof PokemonEntity pokemonEntity))
            return InteractionResult.PASS;
        Pokemon pokemon = pokemonEntity.getPokemon();
        if (pokemon == null) return InteractionResult.PASS;

        if (pokemon.getOwnerUUID() == null) {
            player.displayClientMessage(Component.translatable("message.shadowedhearts.shadow_scale.not_owned").withStyle(net.minecraft.ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (!ShadowAspectUtil.hasShadowAspect(pokemon)) {
            player.displayClientMessage(Component.translatable("message.shadowedhearts.shadow_scale.not_shadow").withStyle(net.minecraft.ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }

        if (player instanceof ServerPlayer serverPlayer) {
            int heartGaugeMax = HeartGaugeConfig.getMax(pokemon);
            MoveSet moveSet = pokemon.getMoveSet();

            List<Move> rerollableMoves = moveSet.getMoves().stream().filter(move -> move.getType().equals(Shadowedhearts.SH_SHADOW_TYPE)).toList();


            MoveSelectCallbacks.INSTANCE.create(serverPlayer, rerollableMoves, move -> {
                //pokemon.exchangeMove(move.getTemplate(), )
                return null;
            });

        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand interactionHand) {
        return super.use(level, player, interactionHand);
    }
}
