package dunkmania101.spatialharvesters.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

public class KeyboardHelper {
    private static final long WIN = Minecraft.getInstance().func_228018_at_().getHandle();

    @OnlyIn(Dist.CLIENT)
    public static boolean isLShift() {
        return InputMappings.isKeyDown(WIN, GLFW.GLFW_KEY_LEFT_SHIFT);
    }

    @OnlyIn(Dist.CLIENT)
    public static boolean isRShift() {
        return InputMappings.isKeyDown(WIN, GLFW.GLFW_KEY_RIGHT_SHIFT);
    }
}
