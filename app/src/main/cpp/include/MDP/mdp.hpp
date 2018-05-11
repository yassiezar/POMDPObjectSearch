#include <chrono>

#include <MDP/model.hpp>

#include <AndroidMDP/MDP/SparseModel.hpp>

namespace MDPNameSpace
{
    class MDP
    {
    public:
        MDP();

        void solve();

        size_t getAction(size_t);

        size_t setTarget(size_t);

    private:
        size_t target = 0;
        Model fullModel;
        AndroidMDP::MDP::SparseModel sparseModel;
    };
}