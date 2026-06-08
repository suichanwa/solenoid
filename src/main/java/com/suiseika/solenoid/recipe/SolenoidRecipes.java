package com.suiseika.solenoid.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.suiseika.solenoid.Solenoid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Optional;
import java.util.function.Supplier;

public class SolenoidRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Solenoid.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Solenoid.MODID);

    public static final Supplier<RecipeType<CrushingRecipe>> CRUSHING_TYPE = RECIPE_TYPES.<RecipeType<CrushingRecipe>>register("crushing", () -> new RecipeType<CrushingRecipe>() {
        @Override
        public String toString() {
            return "crushing";
        }
    });

    public static final Supplier<RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER = RECIPE_SERIALIZERS.register("crushing", () -> {
        MapCodec<CrushingRecipe> codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(CrushingRecipe::ingredient),
                ItemStackTemplate.CODEC.fieldOf("result").forGetter(CrushingRecipe::result),
                Codec.INT.fieldOf("energy").forGetter(CrushingRecipe::energy),
                Codec.INT.fieldOf("time").forGetter(CrushingRecipe::time)
        ).apply(inst, CrushingRecipe::new));

        StreamCodec<RegistryFriendlyByteBuf, CrushingRecipe> streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, CrushingRecipe::ingredient,
                ItemStackTemplate.STREAM_CODEC, CrushingRecipe::result,
                ByteBufCodecs.INT, CrushingRecipe::energy,
                ByteBufCodecs.INT, CrushingRecipe::time,
                CrushingRecipe::new
        );

        return new RecipeSerializer<>(codec, streamCodec);
    });

    public static final Supplier<RecipeType<SeparatingRecipe>> SEPARATING_TYPE = RECIPE_TYPES.<RecipeType<SeparatingRecipe>>register("separating", () -> new RecipeType<SeparatingRecipe>() {
        @Override
        public String toString() {
            return "separating";
        }
    });

    public static final Supplier<RecipeSerializer<SeparatingRecipe>> SEPARATING_SERIALIZER = RECIPE_SERIALIZERS.register("separating", () -> {
        MapCodec<SeparatingRecipe> codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(SeparatingRecipe::ingredient),
                SeparationOutput.CODEC.listOf().optionalFieldOf("outputs", java.util.List.of()).forGetter(SeparatingRecipe::outputs),
                Codec.INT.fieldOf("energy").forGetter(SeparatingRecipe::energy),
                Codec.INT.fieldOf("time").forGetter(SeparatingRecipe::time),
                // Legacy fields for backward compatibility
                ItemStackTemplate.CODEC.optionalFieldOf("result").forGetter(r -> Optional.empty()),
                ItemStackTemplate.CODEC.optionalFieldOf("secondary").forGetter(r -> Optional.empty()),
                Codec.FLOAT.optionalFieldOf("secondaryChance", 0.0f).forGetter(r -> 0.0f)
        ).apply(inst, (ing, outputs, energy, time, res, sec, secChance) -> {
            if (outputs.isEmpty() && res.isPresent()) {
                java.util.List<SeparationOutput> legacy = new java.util.ArrayList<>();
                legacy.add(new SeparationOutput(res.get(), 1.0f));
                sec.ifPresent(s -> legacy.add(new SeparationOutput(s, secChance)));
                return new SeparatingRecipe(ing, legacy, energy, time);
            }
            return new SeparatingRecipe(ing, outputs, energy, time);
        }));

        StreamCodec<RegistryFriendlyByteBuf, SeparatingRecipe> streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SeparatingRecipe::ingredient,
                SeparationOutput.STREAM_CODEC.apply(ByteBufCodecs.list()), SeparatingRecipe::outputs,
                ByteBufCodecs.INT, SeparatingRecipe::energy,
                ByteBufCodecs.INT, SeparatingRecipe::time,
                SeparatingRecipe::new
        );

        return new RecipeSerializer<>(codec, streamCodec);
    });
}
