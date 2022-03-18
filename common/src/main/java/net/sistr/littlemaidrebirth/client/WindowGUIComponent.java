package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.util.math.MatrixStack;
import net.sistr.littlemaidmodelloader.client.screen.GUIElement;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;

public class WindowGUIComponent extends GUIElement {
    private final ImmutableList<GUIElement> elements;
    private boolean click;
    private int prevX;
    private int prevY;
    private int clickAtX;
    private int clickAtY;

    public WindowGUIComponent(int x, int y, int width, int height, Collection<GUIElement> elements) {
        super(width, height);
        this.x = x;
        this.y = y;
        this.elements = ImmutableList.copyOf(elements);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        matrices.push();
        matrices.translate(this.x, this.y, 0);
        for (GUIElement element : elements) {
            element.render(matrices, mouseX - this.x, mouseY - this.y, delta);
        }
        matrices.pop();
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        for (GUIElement element : elements) {
            element.mouseMoved(mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (GUIElement element : elements) {
            if (element.mouseClicked(mouseX - this.x, mouseY - this.y, button)) {
                return true;
            }
        }
        if (this.x < mouseX && mouseX < this.x + this.width
                && this.y < mouseY && mouseY < this.y + this.height) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                click = true;
                prevX = this.x;
                prevY = this.y;
                clickAtX = (int) mouseX;
                clickAtY = (int) mouseY;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GUIElement element : elements) {
            if (element.mouseReleased(mouseX - this.x, mouseY - this.y, button)) {
                return true;
            }
        }
        if (this.x < mouseX && mouseX < this.x + this.width
                && this.y < mouseY && mouseY < this.y + this.height) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                click = false;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        for (GUIElement element : elements) {
            if (element.mouseDragged(mouseX - this.x, mouseY - this.y, button, deltaX, deltaY)) {
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && click) {
            this.x = prevX + (int) (mouseX - clickAtX);
            this.y = prevY + (int) (mouseY - clickAtY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (GUIElement element : elements) {
            if (element.mouseScrolled(mouseX - this.x, mouseY - this.y, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        for (GUIElement element : elements) {
            if (element.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        for (GUIElement element : elements) {
            if (element.keyReleased(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (GUIElement element : elements) {
            if (element.charTyped(chr, modifiers)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean changeFocus(boolean lookForwards) {
        for (GUIElement element : elements) {
            if (element.changeFocus(lookForwards)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        for (GUIElement element : elements) {
            if (element.isMouseOver(mouseX - this.x, mouseY - this.y)) {
                return true;
            }
        }
        return false;
    }
}
