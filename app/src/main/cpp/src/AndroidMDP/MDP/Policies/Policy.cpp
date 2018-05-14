#include <AndroidMDP/MDP/Policies/Policy.hpp>
#include <AndroidMDP/Util/Probability.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        Policy::Policy(const size_t s, const size_t a) : PolicyInterface::Base(s, a), policy_(S, A)
        {
            policy_.fill(1.0/getA());
        }

        size_t Policy::sampleAction(const size_t &s) const
        {
            return sampleProbability(A, policy_.row(s), rand_);
        }

        Matrix2D Policy::getPolicy() const
        {
            return policy_;
        }

        void Policy::setValueFunction(const ValueFunction &v)
        {
            const auto &actions = v.actions;
            policy_.fill(0.0);

            for(size_t s = 0; s < S; s ++)
            {
                policy_(s, actions[s]) = 1.0;
            }
        }
    }
}