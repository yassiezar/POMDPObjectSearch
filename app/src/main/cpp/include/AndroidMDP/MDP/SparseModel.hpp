#ifndef ANDROIDMDP_MDP_SPARSEMODEL_HPP
#define ANDROIDMDP_MDP_SPARSEMODEL_HPP

#include <AndroidMDP/Util/Seed.hpp>
#include <AndroidMDP/MDP/Types.hpp>
#include <AndroidMDP/Util/Probability.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        class SparseModel
        {
        public:
            using TransitionTable = SparseMatrix3D;
            using RewardTable = SparseMatrix2D;

            template <typename M, typename = std::enable_if_t<is_model<M>::value>>
            SparseModel(const M &model);

            SparseModel(const size_t, const size_t, const double);

            void setDiscount(double d) { discount_ = d; }

            size_t getS() const;
            size_t getA() const;
            double getDiscount() const;

            double getTransitionProbability(size_t, size_t, size_t) const;
            double getExpectedReward(size_t, size_t, size_t) const;

            void setTransitionFunction(const AndroidMDP::SparseMatrix3D&);
            void setRewardFunction(const AndroidMDP::SparseMatrix2D&);

            bool isTerminal(size_t) const;
            std::tuple<size_t, double> sampleSR(size_t, size_t) const;

        private:
            size_t S, A;
            double discount_;

            TransitionTable transitions_;
            RewardTable rewards_;

            mutable std::default_random_engine rand_;
        };
    }
}

#endif
