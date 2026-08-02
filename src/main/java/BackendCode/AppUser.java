package BackendCode;

/**
 * Somebody who can sign in, and what they are allowed to do.
 *
 * @author @Barath-Grama
 */
public final class AppUser {

    /**
     * What a signed-in user may do.
     * <p>
     * Deliberately coarse. The distinction that matters for a rental desk is between
     * the person who runs the business and the person behind the counter: taking
     * bookings is everyday work, writing off a customer's bill or deleting an owner
     * and their whole fleet is not.
     */
    public enum Role {
        /** Everything, including clearing balances and removing owners. */
        ADMIN,
        /** Day-to-day desk work: add and edit records, book and return cars. */
        STAFF
    }

    private final int id;
    private final String username;
    private final Role role;

    public AppUser(int id, String username, Role role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public Role getRole() {
        return role;
    }

    /**
     * @return true if this user may clear money owed and delete owners or customers
     */
    public boolean canManageAccounts() {
        return role == Role.ADMIN;
    }

    @Override
    public String toString() {
        return username + " (" + role + ")";
    }
}
