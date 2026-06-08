package com.suiseika.solenoid.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

public record SeparationOutput(ItemStackTemplate template, float chance) {
    public static final Codec<SeparationOutput> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(SeparationOutput::template),
            Codec.FLOAT.optionalFieldOf("chance", 1.0f).forGetter(SeparationOutput::chance)
    ).apply(inst, SeparationOutput::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeparationOutput> STREAM_CODEC = StreamCodec.composite(
            ItemStackTemplate.STREAM_CODEC, SeparationOutput::template,
            ByteBufCodecs.FLOAT, SeparationOutput::chance,
            SeparationOutput::new
    );

    public ItemStack create() {
        return template.create();
    }
}
