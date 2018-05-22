#ifndef MDP_MDP_HPP
#define MDP_MDP_HPP

#include <chrono>

#include <jni.h>
#include <android/log.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

#include <MDP/model.hpp>

#include <AndroidMDP/MDP/SparseModel.hpp>
#include <AndroidMDP/MDP/Algorithms/ValueIteration.hpp>
#include <AndroidMDP/MDP/Policies/Policy.hpp>

#include <nlohmann/json.hpp>

namespace MDPNameSpace
{
    using json = nlohmann::json;

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

#endif