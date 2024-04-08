package ut.com.newlife.ta4j;

import org.junit.Test;
import com.newlife.ta4j.api.MyPluginComponent;
import com.newlife.ta4j.impl.MyPluginComponentImpl;

import static org.junit.Assert.assertEquals;

public class MyComponentUnitTest {
    @Test
    public void testMyName() {
        MyPluginComponent component = new MyPluginComponentImpl(null);
        assertEquals("names do not match!", "myComponent", component.getName());
    }
}