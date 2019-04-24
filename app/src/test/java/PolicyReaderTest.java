import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.example.jaycee.pomdpobjectsearch.Objects;
import com.example.jaycee.pomdpobjectsearch.policy.POMDPPolicy;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PolicyReaderTest
{
    private Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void testPolicyReaderValid()
    {
        POMDPPolicy policy = new POMDPPolicy(context);
        policy.setTarget(Objects.Observation.T_TIGER, 2);
        Assert.assertEquals(policy.getPolicy().get(0).get(0).action, 0);
    }
}
