#ifndef ANDROIDMDP_MDP_POLICY_HPP
#define ANDROIDMDP_MDP_POLICY_HPP

#include <vector>
#include <tuple>

#include <AndroidMDP/MDP/Types.hpp>
#include <AndroidMDP/MDP/Policies/PolicyInterface.hpp>

namespace AndroidMDP
{
    namespace MDP
    {
        class Policy : public PolicyInterface
        {
        public:
            using PolicyTable = Matrix2D;

            Policy(size_t, size_t);

            virtual size_t sampleAction(const size_t&) const override;

            virtual Matrix2D getPolicy() const override;

            void setValueFunction(const ValueFunction &v);

        private:
            PolicyTable policy_;
        };
    }
}

#endif
