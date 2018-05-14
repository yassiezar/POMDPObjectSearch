#include <chrono>

#include <MDP/model.hpp>

#include <AndroidMDP/MDP/SparseModel.hpp>
#include <AndroidMDP/MDP/Algorithms/ValueIteration.hpp>
#include <AndroidMDP/MDP/Policies/Policy.hpp>

namespace MDPNameSpace
{
    class MDP
    {
    public:
        MDP();

        void solve(const size_t);

        size_t getAction(size_t);
        void setTarget(size_t);

    private:
        size_t target = 0;
        Model fullModel;
        AndroidMDP::MDP::SparseModel sparseModel;
        AndroidMDP::MDP::Policy policy;
    };
}