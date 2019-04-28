import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import com.example.jaycee.pomdpobjectsearch.Objects;
import com.example.jaycee.pomdpobjectsearch.policy.POMDPPolicy;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class PolicyReaderTestInstrumental
{
    private Context context;

    @Before
    public void setup()
    {
        context = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @Test
    public void testPolicyReaderValid()
    {
        POMDPPolicy policy = new POMDPPolicy(context);
        policy.setTarget(Objects.Observation.T_TIGER, 2);
        Assert.assertEquals(policy.getPolicy().get(0).get(0).action, 0);
    }
}
