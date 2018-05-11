#include <MDP/mdp.hpp>

namespace MDPNameSpace
{
    MDP::MDP() : fullModel(Model(target)),
        sparseModel(AndroidMDP::MDP::SparseModel(fullModel.getS(), fullModel.getA(), fullModel.getDiscount()))
        {
            size_t a = fullModel.getA();
            size_t s = fullModel.getS();

            AndroidMDP::SparseMatrix3D transitions(a, AndroidMDP::SparseMatrix2D(s, s));
            AndroidMDP::SparseMatrix2D rewards(s, a);

            for(size_t sn = 0; sn < s; sn ++)
            {
                for(size_t an = 0; an < a; an ++)
                {
                    for(size_t sn1 = 0; sn1 < s; sn1 ++)
                    {
                        const double p = fullModel.getTransitionProbability(sn, an, sn1);
                        transitions[an].insert(sn, sn1) = p;
                        rewards.coeffRef(sn, an) += p * fullModel.getExpectedReward(sn, an, sn1);
                    }
                }
            }
            sparseModel.setTransitionFunction(transitions);
            sparseModel.setRewardFunction(rewards);
        }
}