package org.netbeans.lib.awtextra;

import java.io.Serializable;

/**
 * Where a component sits inside an {@link AbsoluteLayout}, in pixels.
 * <p>
 * This project used to pull this class from the "Absolute Layout" library that
 * NetBeans registers as an IDE-global library. Nothing in the repository defined
 * where that library lives, so a checkout could only be built from inside a NetBeans
 * that happened to have it -- plain ant or javac could not resolve the classpath.
 * Keeping the two classes in the source tree makes the project build anywhere.
 *
 * @author @Barath-Grama
 */
public class AbsoluteConstraints implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Pixels from the left edge of the container's insets. */
    public int x;
    /** Pixels from the top edge of the container's insets. */
    public int y;
    /** Width in pixels, or -1 to use the component's preferred width. */
    public int width = -1;
    /** Height in pixels, or -1 to use the component's preferred height. */
    public int height = -1;

    /**
     * Places a component at the given position, at its own preferred size.
     */
    public AbsoluteConstraints(int x, int y) {
        this(x, y, -1, -1);
    }

    /**
     * Places a component at the given position and size. A width or height of -1
     * means "use whatever the component prefers for that dimension".
     */
    public AbsoluteConstraints(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    @Override
    public String toString() {
        return "AbsoluteConstraints{x=" + x + ", y=" + y
                + ", width=" + width + ", height=" + height + '}';
    }
}
