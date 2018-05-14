#include <MDP/mdp.hpp>

namespace MDPNameSpace
{
    MDP::MDP() : fullModel(Model()),
        sparseModel(AndroidMDP::MDP::SparseModel(fullModel.getS(), fullModel.getA(), fullModel.getDiscount())),
        policy(AndroidMDP::MDP::Policy(fullModel.getS(), fullModel.getA()))
    {
    }

    void MDP::solve(const size_t horizon)
    {
        AndroidMDP::MDP::ValueIteration solver(horizon);

        std::tuple<double, AndroidMDP::MDP::ValueFunction, AndroidMDP::MDP::QFunction> solution = solver(sparseModel);
        policy.setValueFunction(std::get<1>(solution));
    }

    size_t MDP::getAction(const size_t state)
    {
        return policy.sampleAction(state);
    }

    void MDP::setTarget(const size_t target)
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