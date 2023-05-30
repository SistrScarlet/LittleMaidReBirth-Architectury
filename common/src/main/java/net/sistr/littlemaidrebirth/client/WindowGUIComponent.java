package net.sistr.littlemaidrebirth.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.gui.DrawContext;
import net.sistr.littlemaidmodelloader.client.screen.GUIElement;
import net.sistr.littlemaidrebirth.util.Pos2d;
import org.lwjgl.glfw.GLFW;

import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;

public class WindowGUIComponent extends GUIElement {
    private final ImmutableList<GUIElement> elements;
    private boolean click;
    private int prevX;
    private int prevY;
    private int clickAtX;
    private int clickAtY;
    private final List<Pos2d> prevElementsPos;

    public WindowGUIComponent(int x, int y, int width, int height, Collection<GUIElement> elements) {
        super(width, height);
        this.x = x;
        this.y = y;
        this.elements = ImmutableList.copyOf(elements);
        this.prevElementsPos = elements.stream().map(e -> new Pos2d(e.getX(), e.getY())).collect(Collectors.toList());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        for (GUIElement element : elements) {
            element.render(context, mouseX, mouseY, delta);
        }
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
            if (element.mouseClicked(mouseX, mouseY, button)) {
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
                ListIterator<Pos2d> iterator = prevElementsPos.listIterator();
                for (GUIElement element : elements) {
                    iterator.next();
                    iterator.set(new Pos2d(element.getX(), element.getY()));
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        for (GUIElement element : elements) {
            if (element.mouseReleased(mouseX, mouseY, button)) {
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
            if (element.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT && click) {
            //deltaXY使うと、intにキャストする都合上で誤差がアホになる
            int dX = (int) (mouseX - clickAtX);
            int dY = (int) (mouseY - clickAtY);
            this.x = prevX + dX;
            this.y = prevY + dY;
            ListIterator<Pos2d> iterator = prevElementsPos.listIterator();
            for (GUIElement element : elements) {
                Pos2d prev = iterator.next();
                element.setPos(prev.x() + dX, prev.y() + dY);
            }
            for (GUIElement element : elements) {
                element.setPos(element.getX() + (int) deltaX, element.getY() + (int) deltaY);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        for (GUIElement element : elements) {
            if (element.mouseScrolled(mouseX, mouseY, amount)) {
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
    public boolean isMouseOver(double mouseX, double mouseY) {
        for (GUIElement element : elements) {
            if (element.isMouseOver(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setPos(int x, int y) {
        for (GUIElement element : elements) {
            element.setPos(element.getX() + (x - this.x), element.getY() + (y - this.y));
        }
        super.setPos(x, y);
    }
}
