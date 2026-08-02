package org.netbeans.lib.awtextra;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.io.Serializable;
import java.util.HashMap;

/**
 * A layout manager that puts every component exactly where its
 * {@link AbsoluteConstraints} say, in pixels, and never moves it.
 * <p>
 * See {@link AbsoluteConstraints} for why these two classes live in the source tree
 * rather than coming from the NetBeans IDE library.
 *
 * @author @Barath-Grama
 */
public class AbsoluteLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = 1L;

//    declared as HashMap rather than Map because the field is part of a
//    Serializable class and Map itself is not a serializable type
    private final HashMap<Component, AbsoluteConstraints> constraints = new HashMap<>();

    @Override
    public void addLayoutComponent(String name, Component comp) {
        throw new IllegalArgumentException("AbsoluteLayout needs an AbsoluteConstraints, not a name");
    }

    @Override
    public void addLayoutComponent(Component comp, Object constr) {
        if (!(constr instanceof AbsoluteConstraints)) {
            throw new IllegalArgumentException("constraint must be an AbsoluteConstraints");
        }
        constraints.put(comp, (AbsoluteConstraints) constr);
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        constraints.remove(comp);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        return sizeToFitEverything(parent);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return sizeToFitEverything(parent);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    @Override
    public float getLayoutAlignmentX(Container target) {
        return 0.5f;
    }

    @Override
    public float getLayoutAlignmentY(Container target) {
        return 0.5f;
    }

    @Override
    public void invalidateLayout(Container target) {
//        nothing is cached, so there is nothing to throw away
    }

    @Override
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component comp = parent.getComponent(i);
                AbsoluteConstraints constraint = constraints.get(comp);
                if (constraint == null) {
                    continue;
                }
                Dimension size = sizeOf(comp, constraint);
                comp.setBounds(insets.left + constraint.x, insets.top + constraint.y,
                        size.width, size.height);
            }
        }
    }

    /**
     * The container has to be big enough for the bottom-right corner of whichever
     * component reaches furthest, plus the container's own insets.
     */
    private Dimension sizeToFitEverything(Container parent) {
        synchronized (parent.getTreeLock()) {
            int width = 0;
            int height = 0;
            for (int i = 0; i < parent.getComponentCount(); i++) {
                Component comp = parent.getComponent(i);
                AbsoluteConstraints constraint = constraints.get(comp);
                if (constraint == null) {
                    continue;
                }
                Dimension size = sizeOf(comp, constraint);
                width = Math.max(width, constraint.x + size.width);
                height = Math.max(height, constraint.y + size.height);
            }
            Insets insets = parent.getInsets();
            return new Dimension(width + insets.left + insets.right,
                    height + insets.top + insets.bottom);
        }
    }

    /**
     * A width or height of -1 in the constraint means the component picks that
     * dimension itself.
     */
    private Dimension sizeOf(Component comp, AbsoluteConstraints constraint) {
        Dimension preferred = comp.getPreferredSize();
        int width = constraint.width < 0 ? preferred.width : constraint.width;
        int height = constraint.height < 0 ? preferred.height : constraint.height;
        return new Dimension(width, height);
    }
}
