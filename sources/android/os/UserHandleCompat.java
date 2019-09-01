package android.os;

public class UserHandleCompat {
    public static final UserHandle SYSTEM = new UserHandle(0);

    public static UserHandle of(int userId) {
        return userId == 0 ? SYSTEM : new UserHandle(userId);
    }
}
