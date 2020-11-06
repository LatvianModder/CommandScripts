package dev.latvian.kubejs.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.latvian.kubejs.KubeJS;
import dev.latvian.kubejs.docs.ID;
import dev.latvian.kubejs.server.ServerJS;
import dev.latvian.kubejs.text.Text;
import dev.latvian.kubejs.text.TextString;
import dev.latvian.kubejs.text.TextTranslate;
import dev.latvian.kubejs.world.WorldJS;
import dev.latvian.mods.rhino.Wrapper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.EndNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.NumberNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.potion.Effect;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.IForgeRegistryEntry;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author LatvianModder
 */
public class UtilsJS
{
	public static final Pattern REGEX_PATTERN = Pattern.compile("\\/(.*)\\/([a-z]*)");

	@Nullable
	public static Pattern regex(String string, boolean strong)
	{
		Matcher matcher = REGEX_PATTERN.matcher(string);

		if (matcher.find())
		{
			int flags = 0;
			String f = matcher.group(2);

			for (int i = 0; i < f.length(); i++)
			{
				switch (f.charAt(i))
				{
					case 'd':
						flags |= Pattern.UNIX_LINES;
						break;
					case 'i':
						flags |= Pattern.CASE_INSENSITIVE;
						break;
					case 'x':
						flags |= Pattern.COMMENTS;
						break;
					case 'm':
						flags |= Pattern.MULTILINE;
						break;
					case 's':
						flags |= Pattern.DOTALL;
						break;
					case 'u':
						flags |= Pattern.UNICODE_CASE;
						break;
					case 'U':
						flags |= Pattern.UNICODE_CHARACTER_CLASS;
						break;
				}
			}

			return Pattern.compile(matcher.group(1), flags);
		}
		else if (!strong)
		{
			return Pattern.compile(string);
		}

		return null;
	}

	public static final Random RANDOM = new Random();

	public static void init()
	{
	}

	@SuppressWarnings("unchecked")
	public static <T> T cast(Object o)
	{
		return (T) o;
	}

	public static void queueIO(Runnable runnable)
	{
		/*FIXME: ThreadedFileIOBase.getThreadedIOInstance().queueIO(() -> {

			try
			{
				runnable.run();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}

			return false;
		});
		 */

		try
		{
			runnable.run();
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}

	public static File getFile(String path) throws IOException
	{
		Path path1 = KubeJS.getGameDirectory().resolve(path);
		KubeJS.verifyFilePath(path1);
		return path1.toFile();
	}

	@Nullable
	public static Object copy(@Nullable Object o)
	{
		if (o instanceof Copyable)
		{
			return ((Copyable) o).copy();
		}
		else if (o instanceof JsonElement)
		{
			return JsonUtilsJS.copy((JsonElement) o);
		}
		else if (o instanceof INBT)
		{
			return ((INBT) o).copy();
		}

		return o;
	}

	@Nullable
	public static Object wrap(@Nullable Object o, JSObjectType type)
	{
		//Primitives and already normalized objects
		if (o == null || o instanceof WrappedJS || o instanceof Number || o instanceof Character || o instanceof String || o instanceof Enum || o.getClass().isPrimitive() && !o.getClass().isArray())
		{
			return o;
		}
		else if (o instanceof CharSequence || o instanceof ResourceLocation)
		{
			return o.toString();
		}
		else if (o instanceof Wrapper)
		{
			o = ((Wrapper) o).unwrap();
		}
		// Vanilla text component
		else if (o instanceof ITextComponent)
		{
			Text t = new TextString("");

			List<ITextComponent> list = new ArrayList<>();
			list.add((ITextComponent) o);
			list.addAll(((ITextComponent) o).getSiblings());

			for (ITextComponent c : list)
			{
				Text t1;

				if (c instanceof TranslationTextComponent)
				{
					t1 = new TextTranslate(((TranslationTextComponent) c).getKey(), ((TranslationTextComponent) c).getFormatArgs());
				}
				else
				{
					t1 = new TextString(c.getUnformattedComponentText());
				}

				//TODO: Replace with AT
				t1.bold(c.getStyle().getBold());
				t1.italic(c.getStyle().getItalic());
				t1.underlined(c.getStyle().getUnderlined());
				t1.strikethrough(c.getStyle().getStrikethrough());
				t1.obfuscated(c.getStyle().getObfuscated());
				t1.insertion(c.getStyle().getInsertion());

				ClickEvent ce = c.getStyle().getClickEvent();

				if (ce != null)
				{
					if (ce.getAction() == ClickEvent.Action.RUN_COMMAND)
					{
						t1.click("command:" + ce.getValue());
					}
					else if (ce.getAction() == ClickEvent.Action.SUGGEST_COMMAND)
					{
						t1.click("suggest_command:" + ce.getValue());
					}
					else if (ce.getAction() == ClickEvent.Action.COPY_TO_CLIPBOARD)
					{
						t1.click("copy:" + ce.getValue());
					}
					else if (ce.getAction() == ClickEvent.Action.OPEN_URL)
					{
						t1.click(ce.getValue());
					}
				}

				HoverEvent he = c.getStyle().getHoverEvent();

				if (he != null && he.getAction() == HoverEvent.Action.SHOW_TEXT)
				{
					t1.hover(Text.of(he.getParameter(HoverEvent.Action.SHOW_TEXT)));
				}

				t.append(t1);
			}

			return t;
		}
		// Maps
		else if (o instanceof Map)
		{
			if (!type.checkMap())
			{
				return null;
			}

			MapJS map = new MapJS(((Map) o).size());
			map.putAll((Map) o);
			return map;
		}
		// Lists, Collections, Iterables, GSON Arrays
		else if (o instanceof Iterable)
		{
			if (!type.checkList())
			{
				return null;
			}

			ListJS list = new ListJS();

			for (Object o1 : (Iterable) o)
			{
				list.add(o1);
			}

			return list;
		}
		// Arrays (and primitive arrays are a pain)
		else if (o.getClass().isArray())
		{
			if (type.checkList())
			{
				return ListJS.ofArray(o);
			}
			else
			{
				return null;
			}
		}
		// GSON Primitives
		else if (o instanceof JsonPrimitive)
		{
			return JsonUtilsJS.toPrimitive((JsonPrimitive) o);
		}
		// GSON Objects
		else if (o instanceof JsonObject)
		{
			if (!type.checkMap())
			{
				return null;
			}

			MapJS map = new MapJS(((JsonObject) o).size());

			for (Map.Entry<String, JsonElement> entry : ((JsonObject) o).entrySet())
			{
				map.put(entry.getKey(), entry.getValue());
			}

			return map;
		}
		// GSON and NBT Null
		else if (o instanceof JsonNull || o instanceof EndNBT)
		{
			return null;
		}
		// NBT
		else if (o instanceof CompoundNBT)
		{
			if (!type.checkMap())
			{
				return null;
			}

			CompoundNBT nbt = (CompoundNBT) o;

			MapJS map = new MapJS(nbt.size());

			for (String s : nbt.keySet())
			{
				map.put(s, nbt.get(s));
			}

			return map;
		}
		else if (o instanceof NumberNBT)
		{
			return ((NumberNBT) o).getAsNumber();
		}
		else if (o instanceof StringNBT)
		{
			return ((StringNBT) o).getString();
		}

		return o;
	}

	public static <T> FieldJS<T> getField(String className, String fieldName)
	{
		try
		{
			return getField(Class.forName(className), fieldName);
		}
		catch (Throwable ex)
		{
			return new FieldJS<>(null);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T> FieldJS<T> getField(Class className, String fieldName)
	{
		try
		{
			return new FieldJS<>(ObfuscationReflectionHelper.findField(className, fieldName));
		}
		catch (Throwable ex)
		{
			return new FieldJS<>(null);
		}
	}

	public static int parseInt(@Nullable Object object, int def)
	{
		if (object == null)
		{
			return def;
		}
		else if (object instanceof Number)
		{
			return ((Number) object).intValue();
		}

		try
		{
			String s = object.toString();

			if (s.isEmpty())
			{
				return def;
			}

			return Integer.parseInt(s);
		}
		catch (Exception ex)
		{
			return def;
		}
	}

	public static double parseDouble(@Nullable Object object, double def)
	{
		if (object == null)
		{
			return def;
		}
		else if (object instanceof Number)
		{
			return ((Number) object).doubleValue();
		}

		try
		{
			String s = object.toString();

			if (s.isEmpty())
			{
				return def;
			}

			return Double.parseDouble(String.valueOf(object));
		}
		catch (Exception ex)
		{
			return def;
		}
	}

	public static Stat<ResourceLocation> getStat(@ID String id)
	{
		return Stats.CUSTOM.get(getMCID(id));
	}

	public static ToolType getToolType(String id)
	{
		return ToolType.get(id);
	}

	public static WorldJS getWorld(World world)
	{
		if (world.isRemote())
		{
			return getClientWorld();
		}
		else
		{
			return ServerJS.instance.getWorld(world);
		}
	}

	public static WorldJS getClientWorld()
	{
		return KubeJS.instance.proxy.getClientWorld();
	}

	@Nullable
	public static Effect getPotion(@ID String id)
	{
		return ForgeRegistries.POTIONS.getValue(getMCID(id));
	}

	@ID
	public static String getID(@ID @Nullable String s)
	{
		if (s == null || s.isEmpty())
		{
			return "minecraft:air";
		}

		if (s.indexOf(':') == -1)
		{
			return "minecraft:" + s;
		}

		return s;
	}

	public static ResourceLocation getMCID(@ID @Nullable String s)
	{
		if (s == null || s.isEmpty())
		{
			return new ResourceLocation("minecraft:air");
		}

		return new ResourceLocation(s);
	}

	public static String getNamespace(@ID @Nullable String s)
	{
		if (s == null || s.isEmpty())
		{
			return "minecraft";
		}

		int i = s.indexOf(':');
		return i == -1 ? "minecraft" : s.substring(0, i);
	}

	public static String getPath(@ID @Nullable String s)
	{
		if (s == null || s.isEmpty())
		{
			return "air";
		}

		int i = s.indexOf(':');
		return i == -1 ? s : s.substring(i + 1);
	}

	public static <T extends IForgeRegistryEntry<T>> Function<ResourceLocation, Optional<T>> valueGetter(IForgeRegistry<T> registry, @Nullable T def)
	{
		return id -> {
			T value = registry.getValue(id);

			if (value != null && value != def)
			{
				return Optional.of(value);
			}

			return Optional.empty();
		};
	}
}