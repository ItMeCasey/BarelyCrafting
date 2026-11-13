package com.itmecasey.barelycrafting.script;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

/**
 * Gives ModJS scripts a global {@code BarelyCrafting} (the {@link BarelyCraftingApi}). ModJS is optional, so everything here goes
 * through reflection: ModJS offers {@code ExtraBindings.register(BindingProvider)} and the provider's {@code install(type, cx, scope)}
 * adds globals to every script scope. Without ModJS nothing is loaded and nothing fails. Client scripts get no global.
 */
public final class ModJsBridge {
    private static final Logger LOG = Logger.getLogger("barelycrafting");

    private ModJsBridge() {
    }

    /** Installs the binding when ModJS is on the classpath; returns whether it did. */
    public static boolean install(BarelyCraftingApi api) {
        try {
            Class<?> provider = Class.forName("com.itmecasey.modjs.script.BindingProvider");
            Class<?> extra = Class.forName("com.itmecasey.modjs.script.ExtraBindings");
            Class<?> context = Class.forName("org.mozilla.javascript.Context");
            Class<?> scriptable = Class.forName("org.mozilla.javascript.Scriptable");
            Class<?> scriptableObject = Class.forName("org.mozilla.javascript.ScriptableObject");
            Method javaToJs = context.getMethod("javaToJS", Object.class, scriptable);
            Method put = scriptableObject.getMethod("putProperty", scriptable, String.class, Object.class);
            InvocationHandler handler = (proxy, method, args) -> {
                if (method.getName().equals("install") && args != null && args.length == 3) {
                    if (!"CLIENT".equals(String.valueOf(args[0]))) {
                        put.invoke(null, args[2], "BarelyCrafting", javaToJs.invoke(null, api, args[2]));
                    }
                    return null;
                }
                if (method.getName().equals("toString")) {
                    return "BarelyCrafting binding";
                }
                if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                }
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                }
                return null;
            };
            Object binding = Proxy.newProxyInstance(provider.getClassLoader(), new Class<?>[] {provider}, handler);
            extra.getMethod("register", provider).invoke(null, binding);
            LOG.info("Barely Crafting: ModJS scripts can use the BarelyCrafting global");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            LOG.warning("Barely Crafting: could not add the BarelyCrafting global to ModJS: " + e);
            return false;
        }
    }
}
