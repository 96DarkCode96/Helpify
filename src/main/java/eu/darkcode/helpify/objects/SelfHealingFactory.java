package eu.darkcode.helpify.objects;

import jakarta.annotation.Nullable;

public interface SelfHealingFactory<T extends SelfHealingURL> {

    @Nullable T fromSelfHealingURL(String URL);

}