package com.suiseika.solenoid.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.suiseika.solenoid.Solenoid;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class SolenoidRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, Solenoid.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, Solenoid.MODID);

    public static final Supplier<RecipeType<CrushingRecipe>> CRUSHING_TYPE = RECIPE_TYPES.register("crushing", () -> new RecipeType<>() {
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

    public static final Supplier<RecipeType<SeparatingRecipe>> SEPARATING_TYPE = RECIPE_TYPES.register("separating", () -> new RecipeType<>() {
        @Override
        public String toString() {
            return "separating";
        }
    });

    public static final Supplier<RecipeSerializer<SeparatingRecipe>> SEPARATING_SERIALIZER = RECIPE_SERIALIZERS.register("separating", () -> {
        MapCodec<SeparatingRecipe> codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC.fieldOf("ingredient").forGetter(SeparatingRecipe::ingredient),
                ItemStack.CODEC.fieldOf("result").forGetter(SeparatingRecipe::result),
                ItemStack.CODEC.fieldOf("secondary").forGetter(SeparatingRecipe::secondary),
                Codec.FLOAT.fieldOf("secondaryChance").forGetter(SeparatingRecipe::secondaryChance),
                Codec.INT.fieldOf("energy").forGetter(SeparatingRecipe::energy),
                Codec.INT.fieldOf("time").forGetter(SeparatingRecipe::time)
        ).apply(inst, SeparatingRecipe::new));

        StreamCodec<RegistryFriendlyByteBuf, SeparatingRecipe> streamCodec = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, SeparatingRecipe::ingredient,
                ItemStack.STREAM_CODEC, SeparatingRecipe::result,
                ItemStack.STREAM_CODEC, SeparatingRecipe::secondary,
                ByteBufCodecs.FLOAT, SeparatingRecipe::secondaryChance,
                ByteBufCodecs.INT, SeparatingRecipe::energy,
                ByteBufCodecs.INT, SeparatingRecipe::time,
                SeparatingRecipe::new
        );

        return new RecipeSerializer<>(codec, streamCodec);
    });
}
